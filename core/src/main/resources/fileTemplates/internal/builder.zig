const std = @import("std");
const builder = @import("build.zig");
const RetType = @typeInfo(@TypeOf(builder.build)).@"fn".return_type.?;


// serialization types, must be kept in sync with the ZB counterpart
const Serialization = struct {
	const Project = struct {
		/// The root of the project
  		path: []const u8,
		/// The (top-level) steps the project declares
		steps: []const Step,
		/// The modules this project owns, may them be private or public
		modules: []const Module,
		/// Contains the dependencies of the project
		dependencies: []const Dependency,
	};
	const Step = struct {
		/// Self-explanatory
		name: []const u8,
		/// Description given to the step
		description: []const u8,
		/// The kind of step, may be any one declared in `std.Build.Step.Id`
		kind: []const u8,
	};
	const Module = struct {
		/// The root `.zig` file
		root: []const u8,
		/// Public modules are dependable on by other projects
  		public: bool,
		/// Imports of this module
  		imports: []const Import,
	};
	const Import = struct {
		/// Name under the import is available in code
		name: []const u8,
		/// The imported module's owner
  		owner: usize,
		/// The imported module's idx
  		module: usize,
	};
	const Dependency = struct {
		/// The index of the project in the projects list
		project: usize,
		/// Whether the dependency was delcared lazy
		lazy: bool,
	};
};

const Storage = struct {
	projects: std.ArrayList(Project),

	const Project = struct {
  		path: []const u8,
		steps: []const Serialization.Step,
		modules: []const Storage.Module,
		dependencies: []const Dependency,
	};
	const Module = struct {
		module: *std.Build.Module,
		public: bool,
		imports: *std.StringArrayHashMapUnmanaged(*std.Build.Module),
	};
	const Dependency = struct {
		name: []const u8,
		hash: []const u8,
		path: []const u8,
		lazy: bool,
	};
};

const post_writergate = @hasDecl(std, "Io");

pub fn build( b: *std.Build ) !void {
	// run the project's build.zig
	const res = switch ( @typeInfo(RetType) ) {
		.error_union => try builder.build( b ),
		else => builder.build( b )
	};

	// this scope's lifespan is very short, so we can simply use an arena allocator
	var arena: std.heap.ArenaAllocator = .init( b.allocator );
	const alloc = arena.allocator();
	defer arena.deinit();

	// get hold of the process's env vars
	var env = try std.process.getEnvMap( b.allocator );

	// get the port the IDE is listening on
	const port = try std.fmt.parseInt( u16, env.get( "ZIGBRAINS_PORT" ) orelse return error.NoPortGiven, 10 );
	std.log.info( "[ZigBrains:BuildScan] IDE is listening on port {}", .{ port } );

	// connect to the port
	var stream = try std.net.tcpConnectToAddress(.{ .in = try std.net.Ip4Address.resolveIp( "127.0.0.1", port ) });
	defer stream.close();

	// gather data
	var storage: Storage = .{ .projects = .init( alloc ) };
	try gatherProjects( b, &storage, alloc );

	const Util = struct {
		pub fn findProjectIndex( strg: *Storage, needle: []const u8 ) ?usize {
			for ( strg.projects.items, 0.. ) |proj, idx| {
				if ( std.mem.eql( u8, needle, proj.path ) ) {
					return idx;
				}
			}
			return null;
		}
		pub fn findProjectModuleIndex( strg: *Storage, projIdx: usize, needle: *std.Build.Module ) ?usize {
			for ( strg.projects.items[projIdx].modules, 0.. ) |mod, idx| {
				if ( mod.module == needle ) {
					return idx;
				}
			}
			return null;
		}
	};

	// process everuyhting to be serializable
	var projects = try alloc.alloc( Serialization.Project, storage.projects.items.len );
	for ( storage.projects.items, 0.. ) |proj, i| {
		// post-process the dependencies
		var dependencies: std.ArrayList(Serialization.Dependency) = try .initCapacity( alloc, proj.dependencies.len );
		for ( proj.dependencies ) |dep| {
			const depProj = Util.findProjectIndex( &storage, dep.path ) orelse continue;
			dependencies.addOneAssumeCapacity().* = .{ .project = depProj, .lazy = dep.lazy };
		}
		// post-process the modules
		const modules: []Serialization.Module = try alloc.alloc( Serialization.Module, proj.modules.len );
		for ( proj.modules, 0.. ) |mud, modIdx| {
			var imports: std.ArrayList(Serialization.Import) = try .initCapacity( alloc, mud.imports.count() );
			var iter = mud.imports.iterator();
			while ( iter.next() ) |imp| {
				const ownerIdx = Util.findProjectIndex( &storage, imp.value_ptr.*.owner.build_root.path.? ) orelse continue;
				const moduleIdx = Util.findProjectModuleIndex( &storage, ownerIdx, imp.value_ptr.* ) orelse continue;
				imports.addOneAssumeCapacity().* = .{
					.name = imp.key_ptr.*,
					.owner = ownerIdx,
					.module = moduleIdx,
				};
			}

			modules[modIdx] = .{
				.root = if (mud.module.root_source_file) |r| r.getDisplayName() else "<null>",
				.public = mud.public,
				.imports = imports.items
			};
		}

		// save the mappings
		projects[i] = .{
			.path = proj.path,
			.steps = proj.steps,
			.modules = modules,
			.dependencies = dependencies.items,
		};
	}

	// serialize
	if (post_writergate) {
		var writer_buf: [1024]u8 = undefined;
		var stream_writer = stream.writer(&writer_buf);
		try std.json.Stringify.value(projects, .{ .whitespace = .indent_4 }, &stream_writer.interface);
		try stream_writer.interface.flush();
	} else {
		try std.json.stringify( projects, .{ .whitespace = .indent_4 }, stream.writer() );
	}

	// hook is done!
	return res;
}

fn gatherProjects( b: *std.Build, storage: *Storage, alloc: std.mem.Allocator ) !void {
	const root_path = b.build_root.path.?;
	// ensure we don't traverse a project twice
	for ( storage.projects.items ) |proj| {
		if ( std.mem.eql( u8, proj.path, root_path ) ) {
			return;
		}
	}

	// gather steps
	var steps = try alloc.alloc( Serialization.Step, b.top_level_steps.count() );
	{
		var i: usize = 0;
		var iter = b.top_level_steps.iterator();
		while ( iter.next() ) |it| {
			const topLevel = it.value_ptr.*;
			// usually they have either 0 or 1 dependencies, being the thing that actually runs, in case of 0 its a synthetic step type, usually `uninstall-$x`
			const deps = topLevel.step.dependencies.items;
			steps[i] = .{
				.name = topLevel.step.name,
				.description = topLevel.description,
				.kind = if ( deps.len != 0 ) @tagName( deps[0].id ) else "uninstall",
			};
			i += 1;
		}
	}

	// gather dependencies
	const deps = try alloc.alloc( Storage.Dependency, b.available_deps.len );
	{
		// we do not find the project index here, as that would introduce a dependency on resolving the... dependencies, which can be a problem in case of circular ones
		for ( b.available_deps, 0.. ) |dep, i| {
			// HACK: for now, ignore lazy dependencies
			const obj = b.lazyDependency( dep.@"0", .{ } ) orelse continue;
			deps[i] = .{
				.name = dep.@"0",
				.path = obj.builder.build_root.path.?,
				.hash = dep.@"1",
				.lazy = false,
			};
		}
	}

	// gather modules
	var modules: std.ArrayList(Storage.Module) = try .initCapacity( alloc, b.modules.count() );
	{
		// public modules, we know exactly how many there are, so we prealloc the space for them
		var modIter = b.modules.iterator();
		while ( modIter.next() ) |it| {
			modules.addOneAssumeCapacity().* = .{
				.module = it.value_ptr.*,
				.public = true,
				.imports = &it.value_ptr.*.import_table,
			};
		}

		// private modules
		var iter = b.top_level_steps.iterator();
		while ( iter.next() ) |it| {
			try discoverStepModules( &it.value_ptr.*.*.step, &modules );
		}
	}

	// save the gathered data
	(try storage.projects.addOne()).* = .{
		.path = root_path,
		.modules = modules.items,
		.steps = steps,
		.dependencies = deps,
	};

	// visit the dependencies
	for ( b.available_deps ) |dep| {
		try gatherProjects( (b.lazyDependency( dep.@"0", .{ } ) orelse continue).builder, storage, alloc );
	}
}

fn discoverStepModules( step: *std.Build.Step, modules: *std.ArrayList(Storage.Module) ) !void {
	if ( step.id == .compile ) blk: {
		const compile: *std.Build.Step.Compile = @fieldParentPtr( "step", step );
		// check if a module was already added
		for ( modules.items ) |mod| {
			if ( mod.module == compile.root_module ) {
				break :blk;
			}
		}
		(try modules.addOne()).* = .{
			.module = compile.root_module,
			.public = false,
			.imports = &compile.root_module.import_table,
		};
	}
	for ( step.dependencies.items ) |s| {
		try discoverStepModules( s, modules );
	}
}

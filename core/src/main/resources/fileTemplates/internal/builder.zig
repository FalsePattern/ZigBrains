const std = @import("std");
const builder = @import("build.zig");
const RetType = @typeInfo(@TypeOf(builder.build)).@"fn".return_type.?;


// serialization types, must be kept in sync with the ZB counterpart
const Serialization = struct {
	const Project = struct {
		/// The name of the project
  		name: ?[]const u8,
		/// The version of the project
  		version: ?[]const u8,
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
		/// Whether the dependency was declared lazy
		lazy: bool,
	};
};

// 0.14 compat
const postWritergate = @hasDecl( std, "Io" );
const newArrayLists = @hasDecl(std, "array_list");
const ArrayList = if ( newArrayLists ) std.ArrayList else std.ArrayListUnmanaged;

// 0.15 compat (IOGate added in 0.16)
const postIOGate = postWritergate and @hasDecl(std.Io, "Threaded");
const IoType = if ( postIOGate ) std.Io else void;

// 0.16 compat (new build apis)
const envInGraph = @hasField(std.Build.Graph, "environ_map");
const ioInGraph = @hasField(std.Build.Graph, "io");

// In-memory representation
const Storage = struct {
	projects: ArrayList(Project),

	const Project = struct {
		name: []const u8,
  		version: ?[]const u8,
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

	// get the port environment variable
    const port_str = if (envInGraph)
        b.graph.environ_map.get("ZIGBRAINS_PORT") orelse return error.NoPortGiven
    else
        std.process.getEnvVarOwned(alloc, "ZIGBRAINS_PORT") catch |e| switch (e) {
            error.EnvironmentVariableNotFound => return error.NoPortGiven,
            else => return e,
        };

    // translate it to an int
    const port = try std.fmt.parseInt( u16, port_str, 10 );
	std.log.info( "[ZigBrains:BuildScan] IDE is listening on port {}", .{ port } );

	// get an Io implementation
	var threaded_io = if (postIOGate and !ioInGraph)
		std.Io.Threaded.init( b.allocator, .{ } )
	else
		{};
	defer if (postIOGate and !ioInGraph) threaded_io.deinit();
    const io = if (postIOGate) if (ioInGraph) b.graph.io else threaded_io.io() else {};

	// connect to the IDE
    var stream = if (postIOGate) netblk: {
		const ip = std.Io.net.IpAddress{
			.ip4 = .loopback(port),
		};
		break :netblk try ip.connect(io, .{
			.mode = .stream,
			.protocol = .tcp,
		});
	} else netblk: {
		break :netblk try std.net.tcpConnectToAddress(.{ .in = try std.net.Ip4Address.resolveIp( "127.0.0.1", port ) });
	};
	defer if (postIOGate) stream.close(io) else stream.close();

	// gather data
	var storage: Storage = .{ .projects = .empty };
	try gatherProjects( b, if ( postIOGate ) io else {}, &storage, "<root>", alloc );

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
			.name = proj.name,
			.version = proj.version,
			.path = proj.path,
			.steps = proj.steps,
			.modules = modules,
			.dependencies = dependencies.items,
		};
	}

	// serialize
	if ( postWritergate ) {
		var writerBuf: [1024]u8 = undefined;
		var streamWriter = if (postIOGate) stream.writer( io, &writerBuf ) else stream.writer( &writerBuf );
		try std.json.Stringify.value( projects, .{ .whitespace = .indent_4 }, &streamWriter.interface );
		try streamWriter.interface.flush();
	} else {
		try std.json.stringify( projects, .{ .whitespace = .indent_4 }, stream.writer() );
	}

	// hook is done!
	return res;
}

fn gatherProjects( b: *std.Build, io: IoType, storage: *Storage, depName: []const u8, alloc: std.mem.Allocator ) !void {
	const root_path = blk: {
		// check if we need to resolve the path
		if ( std.fs.path.isAbsolute( b.build_root.path.? ) ) {
			break :blk b.build_root.path.?;
		}
		// well, we need to, resolve that bad boy!
		if ( postIOGate ) {
			var dir = try std.Io.Dir.cwd().openDir( io, b.build_root.path.?, .{ } );
			defer dir.close( io );
			break :blk try dir.realPathFileAlloc( io, ".", alloc );
		} else {
			var dir = try std.fs.cwd().openDir( b.build_root.path.?, .{ } );
			defer dir.close();
			break :blk try dir.realpathAlloc( alloc, "." );
		}
	};
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
	var modules: ArrayList(Storage.Module) = try .initCapacity( alloc, b.modules.count() );
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
			try discoverStepModules( &it.value_ptr.*.*.step, &modules, alloc );
		}
	}

	// parse the .zon
	var name: []const u8 = depName;
	var version: ?[]const u8 = null;
	parseZigZon( b, io, alloc, root_path, &name, &version ) catch { };

	// save the gathered data
	(try storage.projects.addOne(alloc)).* = .{
		.name = name,
		.version = version,
		.path = root_path,
		.modules = modules.items,
		.steps = steps,
		.dependencies = deps,
	};

	// visit the dependencies
	for ( b.available_deps ) |dep| {
		try gatherProjects( (b.lazyDependency( dep.@"0", .{ } ) orelse continue).builder, io, storage, dep.@"0", alloc );
	}
}

fn discoverStepModules( step: *std.Build.Step, modules: *ArrayList(Storage.Module), alloc: std.mem.Allocator ) !void {
	if ( step.id == .compile ) blk: {
		const compile: *std.Build.Step.Compile = @fieldParentPtr( "step", step );
		// check if a module was already added
		for ( modules.items ) |mod| {
			if ( mod.module == compile.root_module ) {
				break :blk;
			}
		}
		(try modules.addOne( alloc )).* = .{
			.module = compile.root_module,
			.public = false,
			.imports = &compile.root_module.import_table,
		};
	}
	for ( step.dependencies.items ) |s| {
		try discoverStepModules( s, modules, alloc );
	}
}

fn parseZigZon( b: *std.Build, io: IoType, alloc: std.mem.Allocator, root_path: []const u8, name: *[]const u8, version: *?[]const u8 ) !void {
	var file = blk: {
		// well, we need to, resolve that bad boy!
		if ( postIOGate ) {
			var dir = try std.Io.Dir.openDirAbsolute( io, root_path, .{ } );
			defer dir.close( io );
			break :blk try dir.openFile( io, "build.zig.zon", .{ } );
		} else {
			var dir = try std.fs.openDirAbsolute( root_path, .{ } );
			defer dir.close();
			break :blk try dir.openFile( "build.zig.zon", .{ } );
		}
	};
	defer if ( postIOGate )
		file.close( io )
	else
		file.close();

	const stats = try if ( postIOGate )
		file.stat( io )
	else
		file.stat();
	const buffer = try alloc.allocSentinel( u8, stats.size, 0 );
	_ = try if ( postIOGate )
		file.readPositionalAll( io, buffer, 0 )
	else
		file.readAll( buffer );
	const ast = try std.zig.Ast.parse( alloc, buffer, .zon );
	const zoir = try std.zig.ZonGen.generate( alloc, ast, .{ } );

	const ZonIdx = std.zig.Zoir.Node.Index;
	const root = ZonIdx.get( ZonIdx.root, zoir ).struct_literal;

	for ( root.names, 0.. ) |nodeNameNts, idx| {
		const nodeName = nodeNameNts.get( zoir );
		const node = root.vals.at( @intCast( idx ) ).get( zoir );
		if ( std.mem.eql( u8, nodeName, "name" ) ) {
			name.* = b.dupe( node.enum_literal.get( zoir ) );
		} else if ( std.mem.eql( u8, nodeName, "version" ) ) {
			version.* = b.dupe( node.string_literal );
		}
	}
}

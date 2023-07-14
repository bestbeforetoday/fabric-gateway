import { build } from "esbuild";
import { polyfillNode } from "esbuild-plugin-polyfill-node";

build({
	entryPoints: ["dist/core/index.js"],
	bundle: true,
    minify: true,
	outfile: "bundle.js",
    sourcemap: true,
	plugins: [
		polyfillNode({
			globals: {
                buffer: false,
                process: false,
            },
		}),
	],
});

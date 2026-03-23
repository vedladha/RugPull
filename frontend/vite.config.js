import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
    plugins: [react()],
    test: {
        environment: "jsdom",
        setupFiles: "./src/tests/setupTests.js",
        globals: true,
        watch: false,
        reporters: ['default', 'junit'],
        outputFile: {
            junit: './coverage/junit.xml',
        },
        coverage: {
            provider: "v8",
            reporter: ["text", "html", "cobertura"],
            threshold: {
                lines: 80,
                branches: 80,
                functions: 80,
                statements: 80,
            },
            exclude: [
                "node_modules/**",
                "src/main.jsx",
                "src/**/*.test.jsx",
                "eslint.config.js",
                "vite.config.js",
            ]
        }
    }
});

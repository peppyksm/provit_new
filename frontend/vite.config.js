import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

// https://vite.dev/config/
export default defineConfig({
    plugins: [react()],
    resolve: {
        dedupe: ["react", "react-dom"],
    },
    server: {
        port: 5173,
        allowedHosts: true,
        proxy: {
            "/api": {
                target: "http://localhost:8080",
                changeOrigin: true,
                configure: (proxy) => {
                    proxy.on("proxyReq", (proxyReq) => {
                        proxyReq.setHeader("origin", "http://localhost:5173");
                    });
                },
            },
            "/uploads": {
                target: "http://localhost:8080",
                changeOrigin: true,
            },
        },
    },
});

import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// Dev server stays on 3000 so the API's default CORS origin
// (http://localhost:3000) keeps working without extra configuration.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
  },
  build: {
    outDir: "build",
  },
});

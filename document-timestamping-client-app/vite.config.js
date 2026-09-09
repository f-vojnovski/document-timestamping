import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// Port 3000 matches the API's default allowed CORS origin.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
  },
  build: {
    outDir: "build",
  },
});

import type { CapacitorConfig } from "@capacitor/cli";

const config: CapacitorConfig = {
  appId: "ai.akarsh.jarvis",
  appName: "JARVIS Ultimate",
  webDir: ".output/public",
  server: {
    url: "https://jarvisaiassistantbyakarshai.lovable.app",
    androidScheme: "https",
    cleartext: false,
  },
  android: {
    allowMixedContent: false,
    backgroundColor: "#0b0a1a",
  },
};

export default config;
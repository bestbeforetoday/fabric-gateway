module.exports = {
    "roots": [
        "<rootDir>/src"
    ],
    "preset": "ts-jest",
    "testEnvironment": "node",
    "collectCoverage": true,
    "collectCoverageFrom": [
        "src/**/*.{js,jsx,ts,tsx}",
        "!src/**/*.d.ts",
        "!src/protos/*.{ts,js}",
        "!**/node_modules/"
    ],
    "coverageProvider": "v8",
    "testMatch": [
        "**/?(*.)+(spec|test).[jt]s?(x)"
    ],
}

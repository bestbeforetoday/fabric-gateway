/** @type { import('jest').Config } */
const config = {
    roots: ['<rootDir>/src'],
    testEnvironment: 'node',
    transform: {
        '^.+\\.(t|j)sx?$': '@swc/jest',
    },
    collectCoverage: true,
    collectCoverageFrom: ['**/*.[jt]s?(x)', '!**/*.d.ts'],
    coverageProvider: 'v8',
    testMatch: ['**/?(*.)+(spec|test).[jt]s?(x)'],
    verbose: true,
    workerThreads: true,
};

export default config;

process.env.TSCONFIG_ROOT_DIR = __dirname;

module.exports = {
    root: true,
    ignorePatterns: [
        'dist/',
    ],
    extends: [
        '../../node/fabric-gateway-core/.eslintrc.base',
    ],
};

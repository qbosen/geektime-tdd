describe('Args parse', () => {
    it.skip('should parse multi options', () => {
        let schema = {
            logging: option('l', bool()),
            port: option('p', int()),
            directory: option('d', string()),
        };

        let options = parse(schema, ['-l', '-p', '8080', '-d', '/usr/logs']);
        expect(options.logging).toBeTruthy();
        expect(options.port).toEqual(8080);
        expect(options.directory).toEqual('/usr/logs');
    });

    describe('parse', () => {
        it('should call parsers in schema to build option', () => {
            let schema = {
                logging: (args) => args,
                port: (args) => args,
            };

            let option = parse(schema, ["args"]);
            expect(option.logging).toEqual(["args"]);
            expect(option.port).toEqual(["args"]);
        })
    });

    describe('option', () => {
        let opt = option('l', (values) => values);
        it('should fetch values followed by flag', () => {
            expect(opt(['-l', 'a', 'b'])).toEqual(['a', 'b']);
        });
        it('should only fetch values until next flag', () => {
            expect(opt(['-l', 'a', 'b', '-p'])).toEqual(['a', 'b']);
        });

        it('should fetch empty array if no value given', () => {
            expect(opt(['-l'])).toEqual([]);
        });

        it('should fetch undefined if no flag match', () => {
            expect(opt(['-g', 'a', 'b', '-p'])).toBeUndefined();
        });

        it('should call type to handle values', () => {
            let opt = option('l', (values) => 1);
            expect(opt(['-l', 'a', 'b'])).toEqual(1);
        });
    });

});

function option(flag, type) {
    return function (args) {
        let index = args.indexOf(`-${flag}`);
        if (index === -1) {
            return undefined;
        }
        let nextIndex = args.findIndex((v, i) => i > index && /^-[a-zA-Z]+$/.test(v));
        if (nextIndex === -1) {
            nextIndex = args.length;
        }
        return type(args.slice(index + 1, nextIndex));
    };
}

function bool() {

}

function int() {

}

function string() {

}

function parse(schema, args) {
    let options = {};
    for (let key of Object.keys(schema)) {
        options[key] = schema[key](args);
    }
    return options;
}
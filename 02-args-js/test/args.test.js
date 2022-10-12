describe('Args parse', () => {
    it('should parse multi options', () => {
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

    describe('bool', () => {
        let type = bool();
        it('should return true, if empty array given', () => {
            expect(type([])).toBeTruthy();
        });

        it('should return false, if undefined given', () => {
            expect(type(undefined)).toBeFalsy();
        });
        it('should throw exception, if more than 0 value present', () => {
            expect(() => type(["1", "2"])).toThrowError('too many values');
        });
    });

    describe('int', () => {
        let type = int(-17);
        it('should return int value, if single value given', () => {
            expect(type(['1'])).toEqual(1);
        });

        it('should return default value, if undefined given', () => {
            expect(type(undefined)).toEqual(-17);
        });

        it('should throw exception, if no value present', () => {
            expect(() => type([])).toThrowError('too few values');
        });

        it('should throw exception, if more than 1 value present', () => {
            expect(() => type(["1", "2"])).toThrowError('too many values');
        });
    });


    describe('string', () => {
        let type = string('value');
        it('should return string value, if single value given', () => {
            expect(type(['some'])).toEqual('some');
        });

        it('should return default value, if undefined given', () => {
            expect(type(undefined)).toEqual('value');
        });

        it('should throw exception, if no value present', () => {
            expect(() => type([])).toThrowError('too few values');
        });

        it('should throw exception, if more than 1 value present', () => {
            expect(() => type(["hello", "world"])).toThrowError('too many values');
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

function bool(defaultValue = false) {
    return function (args) {
        if (!args) {
            return defaultValue;
        }
        if (args.length > 0) {
            throw 'too many values';
        }
        return true;
    };
}

function int(defaultValue = 0) {
    return function (args) {
        if (!args) return defaultValue;
        if (args.length < 1) throw 'too few values';
        if (args.length > 1) throw 'too many values';
        return parseInt(args[0]);
    };
}

function string(defaultValue = '') {
    return function (args) {
        if (!args) return defaultValue;
        if (args.length < 1) throw 'too few values';
        if (args.length > 1) throw 'too many values';
        return args[0];
    };
}

function parse(schema, args) {
    let options = {};
    for (let key of Object.keys(schema)) {
        options[key] = schema[key](args);
    }
    return options;
}
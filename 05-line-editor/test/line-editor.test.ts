import {describe, expect, it} from "vitest"
import Konva from "konva";
import {LineEditor} from "../src/line-editor";

describe('Line editor', () => {
    //TODO set anchor on Line editor
    it('should add anchor to line editor', () => {
        let line = new Konva.Line({points: [10, 10, 30, 30]});
        let editor = new LineEditor();
        editor.attach(line);

        expect(editor.findOne('.0-anchor').getAttrs()).toMatchObject({x: 10, y: 10});
        expect(editor.findOne('.1-anchor').getAttrs()).toMatchObject({x: 30, y: 30});
    });

    //TODO update anchor when Line update
    //TODO set control point on Line editor
    //TODO update control point when Line update
    //TODO change Line points when dragging anchor
    //TODO add new anchor on editor when dragging control point
    //TODO remove anchor when double click anchor
});
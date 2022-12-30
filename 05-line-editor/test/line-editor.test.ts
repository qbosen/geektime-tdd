import {describe, expect, it} from "vitest"
import Konva from "konva";
import {LineEditor} from "../src/line-editor";
import {DragEvent} from "react";

describe('Line editor', () => {
    it('should add anchor to line editor', () => {
        let line = new Konva.Line({points: [10, 10, 30, 30]});
        let editor = new LineEditor();
        editor.attach(line);

        expect(editor.findOne('.0-anchor').getAttrs()).toMatchObject({x: 10, y: 10});
        expect(editor.findOne('.1-anchor').getAttrs()).toMatchObject({x: 30, y: 30});
    });

    it('should update anchor position when line update', () => {
        let line = new Konva.Line({points: [10, 10, 30, 30]});
        let editor = new LineEditor();
        editor.attach(line);
        line.points([40, 40, 60, 60]);

        expect(editor.findOne('.0-anchor').getAttrs()).toMatchObject({x: 40, y: 40});
        expect(editor.findOne('.1-anchor').getAttrs()).toMatchObject({x: 60, y: 60});
    });

    it('should add control point to line editor', () => {
        let line = new Konva.Line({points: [10, 10, 30, 30]});
        let editor = new LineEditor();
        editor.attach(line);

        expect(editor.findOne('.1-control').getAttrs()).toMatchObject({x: 20, y: 20});
    });

    it('should update control position when line update', () => {
        let line = new Konva.Line({points: [10, 10, 30, 30]});
        let editor = new LineEditor();
        editor.attach(line);
        line.points([40, 40, 60, 60]);

        expect(editor.findOne('.1-control').getAttrs()).toMatchObject({x: 50, y: 50});
    });

    it('should change line points when dragging anchor', () => {
        let line = new Konva.Line({points: [10, 10, 30, 30]});
        let editor = new LineEditor();
        editor.attach(line);

        let anchor = editor.findOne('.1-anchor');
        expect(anchor.draggable()).toEqual(true);

        anchor.x(100).y(100);
        anchor.fire('dragmove', {} as DragEvent);
        expect(line.points()).toEqual([10, 10, 100, 100]);
        expect(editor.findOne('.1-control').getAttrs()).toMatchObject({x: 55, y: 55});

    });

    it('should add new anchor on editor when dragging control point', () => {
        let line = new Konva.Line({points: [10, 10, 30, 30]});
        let editor = new LineEditor();
        editor.attach(line);

        let control = editor.findOne('.1-control');
        expect(control.draggable()).toEqual(true);

        control.x(25).y(30);
        control.fire('dragmove', {} as DragEvent);

        expect(line.points()).toEqual([10, 10, 25, 30, 30, 30]);
        expect(editor.findOne('.1-control').getAttrs()).toMatchObject({x: 17.5, y: 20});
        expect(editor.findOne('.2-control').getAttrs()).toMatchObject({x: 27.5, y: 30});

    });

    it('should remove anchor when double click anchor', () => {
        let line = new Konva.Line({points: [10, 10, 20, 20, 30, 30]});
        let editor = new LineEditor();
        editor.attach(line);

        let anchor = editor.findOne('.1-anchor');
        anchor.fire('dblclick', {} as MouseEvent);

        expect(line.points()).toEqual([10, 10, 30, 30]);
        expect(editor.findOne('.1-anchor').getAttrs()).toMatchObject({x: 30, y: 30});
        expect(editor.findOne('.2-anchor')).toBeUndefined();
        expect(editor.findOne('.2-control')).toBeUndefined();
    });
});
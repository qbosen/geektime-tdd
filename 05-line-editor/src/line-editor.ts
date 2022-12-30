import Konva from "konva";

export class LineEditor extends Konva.Group {

    attach(line: Konva.Line) {
        let points = line.points();
        for (let i = 0; i < points.length / 2; i++) {
            this.add(new Konva.Circle({name: `${i}-anchor`, radius: 10, x: points[i * 2], y: points[i * 2 + 1]}));
        }
    }
}
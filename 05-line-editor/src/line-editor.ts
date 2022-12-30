import Konva from "konva";

export class LineEditor extends Konva.Group {
    private line?: Konva.Line

    attach(line: Konva.Line) {
        let points = line.points();
        for (let i = 0; i < points.length / 2; i++) {
            this.add(new Konva.Circle({name: `${i}-anchor`, radius: 10, x: points[i * 2], y: points[i * 2 + 1]}));
        }

        this.line = line;
        line.on('pointsChange', () => {
            this.update();
        });
    }

    private update() {
        let points = this.line!.points();
        for (let i = 0; i < points.length / 2; i++) {
            this.findOne(`.${i}-anchor`).setAttrs({x: points[i * 2], y: points[i * 2 + 1]});
        }
    }
}
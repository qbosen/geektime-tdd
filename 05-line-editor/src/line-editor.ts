import Konva from "konva";

export class LineEditor extends Konva.Group {
    private line?: Konva.Line
    private pointsCount: number = 0;

    attach(line: Konva.Line) {
        this.line = line;
        line.on('pointsChange', () => {
            this.update();
        });
        this.update();
    }

    private update() {
        let points = this.line!.points();
        let previous = -1;
        for (let i = 0; i < points.length / 2; i++) {
            this.get(i, 'anchor').setAttrs({x: points[i * 2], y: points[i * 2 + 1]});
            if (previous != -1) {
                this.get(i, 'control').setAttrs({
                    x: points[previous * 2] + (points[i * 2] - points[previous * 2]) / 2,
                    y: points[previous * 2 + 1] + (points[i * 2 + 1] - points[previous * 2 + 1]) / 2
                });
            }
            previous = i;
        }
        for (let i = points.length / 2; i < this.pointsCount; i++) {
            this.findOne(`.${i}-anchor`).destroy();
            this.findOne(`.${i}-control`).destroy();
        }
        this.pointsCount = points.length / 2;

    }

    private get(index: number, type: string) {
        return this.findOne(`.${index}-${type}`) || this.create(index, type);
    }

    private create(index: number, type: string) {
        let point = new Konva.Circle({name: `${index}-${type}`, radius: 10, draggable: true});
        if (type === 'anchor') {
            point.on('dragmove', (e) => {
                let points = this.line!.points();
                points[index * 2] = e.target.x();
                points[index * 2 + 1] = e.target.y();
                this.line!.points(points);
            });
            point.on('dblclick', (e) => {
                let points = this.line!.points();
                points.splice(index * 2, 2);
                this.line!.points(points);
            });
        } else {
            point.on('dragmove', (e) => {
                let points = this.line!.points();
                points.splice(index * 2, 0, e.target.x(), e.target.y());
                this.line!.points(points);
            });
        }
        this.add(point);
        return point;
    }
}
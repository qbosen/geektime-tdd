import './App.css'
import {Layer, Line, Stage} from "react-konva";
import {useEffect, useRef} from "react";
import Konva from "konva";
import {LineEditor} from "./line-editor";

function App() {
    const line = useRef<Konva.Line>(null);
    const layer = useRef<Konva.Layer>(null);

    const editor = new LineEditor();
    useEffect(() => {
        if (line.current && layer.current) {
            editor.attach(line.current);
            layer.current.add(editor);
        }
    }, []);

    return (<Stage width={window.innerWidth} height={window.innerHeight}>
        <Layer ref={layer}>
            <Line ref={line} points={[30, 75, 123, 234, 315, 225, 104, 127]} strokeWidth={4} stroke={'black'}/>
        </Layer>
    </Stage>)
}

export default App

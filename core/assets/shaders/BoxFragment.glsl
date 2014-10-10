precision lowp float;

uniform vec4 color;
 
varying vec3 v_pos;
varying vec3 v_vertex;

void main() {
	gl_FragColor = color;
}

precision lowp float;

attribute vec3 position;
attribute vec2 texCoords;
varying vec2 texCoordsV;

void main() {
	texCoordsV = texCoords;
	gl_Position = vec4(position.xy, 0.0, 1.0);
}

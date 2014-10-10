precision lowp float;

uniform sampler2D texture0;

varying vec2 texCoordsV;

void main() {
	vec4 color = texture2D(texture0, texCoordsV);
	gl_FragColor = color;
}

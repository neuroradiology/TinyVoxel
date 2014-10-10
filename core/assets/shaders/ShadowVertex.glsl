precision highp  float;

attribute vec3 position;
attribute float normalColor;
attribute float palette;

uniform mat4 gridTrans;
uniform mat4 worldTrans;
uniform mat4 cameraTrans;

void main() {
	vec4 abs = worldTrans * gridTrans * vec4(position, 1.0);
	vec4 res = cameraTrans * abs;
    gl_Position = res;
}
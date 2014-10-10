precision highp  float;

attribute vec3 position;
 
uniform mat4 cameraTrans;
uniform mat4 modelTrans;
 
varying vec3 v_pos;
varying vec3 v_vertex;
 
void main() {
	v_vertex = position;
	vec4 abs = modelTrans * vec4(position, 1.0);
	v_pos = abs.xyz;
	vec4 res = cameraTrans * abs;
    gl_Position = res;
}
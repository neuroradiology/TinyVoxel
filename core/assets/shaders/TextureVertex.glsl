precision highp  float;

attribute vec3 position;
attribute float normalColor;
attribute float palette;

uniform mat4 gridTrans;
uniform mat4 worldTrans;
uniform mat4 inverseWorldTrans;
uniform mat4 cameraTrans;
uniform vec3 cameraPos;
 
varying vec3 v_vertex;
varying float v_palette;
varying vec3 v_dir;
varying float v_dist;
varying float v_normalColor;

void main() {
	v_vertex = position;
	vec4 abs = worldTrans * gridTrans * vec4(position, 1.0);
	vec3 diff = abs.xyz - cameraPos;
	v_dist = length(diff);
	v_dir = (inverseWorldTrans * vec4(diff, 0.0)).xyz;
	v_palette = palette;
	
	v_normalColor = normalColor;
	
	vec4 res = cameraTrans * abs;
    gl_Position = res;
}
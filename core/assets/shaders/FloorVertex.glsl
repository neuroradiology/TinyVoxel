precision highp  float;

attribute vec3 position;
 
uniform mat4 modelTrans;
uniform mat4 cameraTrans;
uniform vec3 cameraPos;
 
uniform mat4 shadowTrans;
 
varying vec2 v_uv;
varying vec3 v_shadowuv;
varying vec3 v_pos;

void main() {
	vec4 abs = vec4(position, 1.0);
	abs.xz += cameraPos.xz;
	
	vec4 uv = modelTrans * abs;
	v_uv = uv.xz;
	
	v_shadowuv = (shadowTrans * abs).xyz;
	
	v_pos = abs.xyz;
		
    gl_Position = cameraTrans * abs;
}
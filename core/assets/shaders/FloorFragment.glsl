precision highp  float;
 
uniform sampler2D texture;
uniform sampler2D shadowTexture;
uniform float lod;
uniform vec4 backgroundColor;
uniform vec3 cameraPos;

varying vec2 v_uv;
varying vec3 v_shadowuv;
varying vec3 v_pos;

const float FOG = 5.0;


void main(void)
{
	float dist = length(v_pos.xyz - cameraPos);
	
	float shade = texture2D(shadowTexture, v_shadowuv.xy).r * 0.5 + 0.5;
	
	vec4 color = texture2D(texture, v_uv) * shade;
	if (dist > lod - FOG) {
		float distFilter = min((dist - (lod - FOG)) / FOG, 1.0);
		distFilter = 1.0 - distFilter;
		
		color.rgb *= distFilter;
		color.rgb += backgroundColor.rgb * (1.0 - distFilter);
	}
	dist /= lod;
	color.a = 1.0;
	gl_FragColor = color;
}
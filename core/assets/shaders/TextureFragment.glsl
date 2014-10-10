precision highp  float;
 
uniform sampler2D voxelTexture;
uniform vec4 backgroundColor;

varying vec3 v_vertex;
varying float v_palette;
varying vec3 v_dir;
varying float v_dist;
varying float v_normalColor;

uniform vec3 cameraPos;
uniform float paletteSize;
uniform float lod;
uniform float alphaOverwrite;
 
#define TINY_GRID_SIZE 8
#define TINY_GRID_SIZE_F 8.0
#define TINY_GRID_SIZE_HALF_F 4.0
#define MAX_RAY_STEPS 8
#define FOG 5.0

bool outside( vec3 c )
{
  vec3 e = vec3(TINY_GRID_SIZE_F - 1.0) - c;
  e = min(e, c);
  return (e.x < 0.0 || e.y < 0.0 || e.z < 0.0);
}

float rand(vec3 co){
    return fract(sin(dot(co.xyz ,vec3(12.9898,78.233,14.562))) * 43758.5453);
}

vec4 getVoxelValue(vec3 c) {
	vec3 p = c + vec3(0.5, 0, 0);
	p /= TINY_GRID_SIZE_F;
	
	p.x /= TINY_GRID_SIZE_F * TINY_GRID_SIZE_F;
	p.x += p.z / TINY_GRID_SIZE_F;
	p.x += p.y;
	
	p.y = v_palette + 0.5;
	p.y /= paletteSize;
	
	return texture2D(voxelTexture, p.xy).rgba;
}

bool isVoxel(vec3 col) {
	return col.r + col.g + col.b < 3.0;
}

void main(void)
{

	vec4 color = backgroundColor;
	color.a = 1.0;
	
	float dist = v_dist;
	
	if (dist > lod) {
		gl_FragColor = color;
		return;
	}
	
	float palette = v_palette;
	
	vec3 rayPos = mod(v_vertex, 1.0) * TINY_GRID_SIZE_F;
	
	vec3 rayDir = normalize(v_dir);
	float m = max(abs(rayDir.x), max(abs(rayDir.y), abs(rayDir.z)));
	rayDir /= m;
	
	vec3 n = rayPos;
	vec3 floorn = floor(n);
	
	vec3 rayDirTwice = rayDir * TINY_GRID_SIZE_F / float(MAX_RAY_STEPS);
	
	bool finished = false;
	for (int i = 0; i < MAX_RAY_STEPS; i++) {
		color.rgba = getVoxelValue(floorn);
		if (isVoxel(color.rgb)) {
			break;
		}
		
		n += rayDirTwice;
		floorn = floor(n);
		
		if (outside(floorn)) {
			finished = true;
			break;
		}
		
		if (i == MAX_RAY_STEPS-1)
			finished = true;
	}
	
	if (finished) {
		discard;
	}
	
	color.rgb *= color.a;
	
	dist += length(n - rayPos) / TINY_GRID_SIZE_F;	
	
	if (dist > lod - FOG) {
		float distFilter = (dist - (lod - FOG)) / FOG;
		distFilter = 1.0 - distFilter;
		
		color.rgb *= distFilter;
		color.rgb += backgroundColor.rgb * (1.0 - distFilter);
	}
	dist /= lod;
	if (alphaOverwrite == 1.0)
		color.a = dist;
	else
		color.a = alphaOverwrite;
	
	
	gl_FragColor = color;
}
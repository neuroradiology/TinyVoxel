precision highp  float;
 
uniform sampler2D voxelTexture;
uniform vec4 backgroundColor;

varying vec3 v_vertex;
varying float v_palette;
varying vec3 v_dir;
varying float v_dist;
varying float v_normalColor;

uniform float paletteSize;
uniform float lod;
uniform float detailLod;
uniform float alphaOverwrite;
 
#define TINY_GRID_SIZE 8
#define TINY_GRID_SIZE_F 8.0
#define TINY_GRID_SIZE_HALF_F 4.0
#define MAX_RAY_STEPS 16
#define FOG 2.0

bool outside( vec3 c )
{
  vec3 e = vec3(TINY_GRID_SIZE_F - 1.0) - c;
  e = min(e, c);
  return (e.x < 0.0 || e.y < 0.0 || e.z < 0.0);
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
	vec4 color = vec4(0);
	
	float dist = v_dist;
	
	vec3 rayPos = mod(v_vertex, 1.0) * TINY_GRID_SIZE_F;
	
	ivec3 pos = ivec3(floor(rayPos + 0.));
	vec3 fpos = vec3(pos);
	
	vec3 delta = abs(vec3(length(v_dir)) / v_dir);
	
	ivec3 rayStep = ivec3(sign(v_dir));

	vec3 side = (sign(v_dir) * (fpos - rayPos) + (sign(v_dir) * 0.5) + 0.5) * delta; 
	
	bvec3 mask;
	bool finished = false;
	bool first = true;
	for (int i = 0; i < MAX_RAY_STEPS; i++) {
		color.rgba = getVoxelValue(fpos);
		if (isVoxel(color.rgb)) break;
			
		if (side.x < side.y) {
			if (side.x < side.z) {
				side.x += delta.x;
				pos.x += rayStep.x;
				mask = bvec3(true, false, false);
			}
			else {
				side.z += delta.z;
				pos.z += rayStep.z;
				mask = bvec3(false, false, true);
			}
		}
		else {
			if (side.y < side.z) {
				side.y += delta.y;
				pos.y += rayStep.y;
				mask = bvec3(false, true, false);
			}
			else {
				side.z += delta.z;
				pos.z += rayStep.z;
				mask = bvec3(false, false, true);
			}
		}
		
		fpos = vec3(pos);
		
		if (outside(fpos)) {
			finished = true;
			break;
		}
		if (i == MAX_RAY_STEPS-1)
			finished = true;
			
		first = false;
	}
	
	if (finished) {
		discard;
	}
	
	color.rgb *= color.a;
	
	float div = clamp(dist - (detailLod - FOG), 0.0, FOG) / FOG;
	float minDiv = 1.0 - div;
	if (first) {
		color.rgb *= (v_normalColor * minDiv + div);
	} else {
		if (mask.x) {
			color.rgb *= (0.5 * minDiv + div); 
		} 

		if (mask.y) {
		} 

		if (mask.z) {
			color.rgb *= (1.5 * minDiv + div);
		} 

		dist += length(fpos - rayPos) / TINY_GRID_SIZE_F;	
	}
		
	dist /= lod;
	if (alphaOverwrite == 1.0)
		color.a = dist;
	else
		color.a = alphaOverwrite;
	
	gl_FragColor = color;
}
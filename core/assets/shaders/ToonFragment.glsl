precision lowp float;

uniform sampler2D texture0;

varying vec2 texCoordsV;

const float edge_thres = 0.04;

float IsEdge(vec4 mainColor)
{
  if (mainColor.a == 1.0) {
	return 0.0;
  }

  float dtex = 1.0 / 800.0 * (1.0 - mainColor.a);
  float pix[9];
  float delta;

  // read neighboring pixel intensities
  for (int i=-1; i<2; i++) {
   for(int j=-1; j<2; j++) {
    pix[(i+1)*3+j+1] = texture2D(texture0, texCoordsV + vec2(float(i)*dtex, float(j)*dtex)).a;
   }
  }

  // average color differences around neighboring pixels
  delta = (abs(pix[1]-pix[7])+
          abs(pix[5]-pix[3]) +
          abs(pix[0]-pix[8])+
          abs(pix[2]-pix[6])
           )/4.;

  return delta;
}

void main() {
	vec4 mainColor = texture2D(texture0, texCoordsV);
	if (IsEdge(mainColor) < edge_thres) {
		gl_FragColor = mainColor;
	} else {
		gl_FragColor = vec4(0);
	}
}

#ifndef INSTANCING
$input v_fogColor, v_worldPos, v_underwaterRainTime, sPos
#endif

#include <bgfx_shader.sh>

#ifndef INSTANCING
#include <newb/main.sh>
uniform vec4 FogAndDistanceControl;
uniform vec4 ViewPositionAndTime;
#endif

// Falling Stars code By i11212 : https://www.shadertoy.com/view/mdVXDm

highp float hashS(
	highp vec2 x){
return fract(sin(dot(
	x,vec2(11,57)))*4e3);
	}

highp float star(
	highp vec2 x, float time){
x = mul(x, mtxFromCols(vec2(cos(0.0), sin(0.0)), vec2(sin(0.0), -cos(0.5))));
x.y += time*5.0;
highp float shape = (1.0-length(
	fract(x-vec2(0,0.5))-0.5));
x *= vec2(1,0.1);
highp vec2 fr = fract(x);
highp float random = step(hashS(floor(x)),0.01),
	      	  tall = (1.0-(abs(fr.x-0.5)+fr.y*0.5))*random;
return clamp(clamp((shape-random)*step(hashS(
	floor(x+vec2(0,0.05))),.01),0.0,1.0)+tall,0.0,1.0);
}

// 3D Gradient noise from: https://www.shadertoy.com/view/Xsl3Dl
vec3 hash( vec3 p ) // replace this by something better
{
    p = vec3( dot(p,vec3(127.1,311.7, 74.7)),
              dot(p,vec3(269.5,183.3,246.1)),
              dot(p,vec3(113.5,271.9,124.6)));

    return -1.0 + 2.0*fract(sin(p)*43758.5453123);
}
float noise( in vec3 p )
{
    vec3 i = floor( p );
    vec3 f = fract( p );

    vec3 u = f*f*(3.0-2.0*f);

    return mix( mix( mix( dot( hash( i + vec3(0.0,0.0,0.0) ), f - vec3(0.0,0.0,0.0) ),
                          dot( hash( i + vec3(1.0,0.0,0.0) ), f - vec3(1.0,0.0,0.0) ), u.x),
                     mix( dot( hash( i + vec3(0.0,1.0,0.0) ), f - vec3(0.0,1.0,0.0) ),
                          dot( hash( i + vec3(1.0,1.0,0.0) ), f - vec3(1.0,1.0,0.0) ), u.x), u.y),
                mix( mix( dot( hash( i + vec3(0.0,0.0,1.0) ), f - vec3(0.0,0.0,1.0) ),
                          dot( hash( i + vec3(1.0,0.0,1.0) ), f - vec3(1.0,0.0,1.0) ), u.x),
                     mix( dot( hash( i + vec3(0.0,1.0,1.0) ), f - vec3(0.0,1.0,1.0) ),
                          dot( hash( i + vec3(1.0,1.0,1.0) ), f - vec3(1.0,1.0,1.0) ), u.x), u.y), u.z );
}

void main() {
#ifndef INSTANCING
  vec3 viewDir = normalize(v_worldPos);
  bool underWater = v_underwaterRainTime.x > 0.5;
  float rainFactor = v_underwaterRainTime.y;

  float mask = (1.0-1.0*rainFactor)*max(1.0 - 3.0*max(v_fogColor.b, v_fogColor.g), 0.0);

  vec3 zenithCol;
  vec3 horizonCol;
  vec3 horizonEdgeCol;
  if (underWater) {
    vec3 fogcol = getUnderwaterCol(v_fogColor);
    zenithCol = fogcol;
    horizonCol = fogcol;
    horizonEdgeCol = fogcol;
  } else {
    vec3 fs = getSkyFactors(v_fogColor);
    zenithCol = getZenithCol(rainFactor, v_fogColor, fs);
    horizonCol = getHorizonCol(rainFactor, v_fogColor, fs);
    horizonEdgeCol = getHorizonEdgeCol(horizonCol, rainFactor, v_fogColor);
  }

  vec3 skyColor = nlRenderSky(horizonEdgeCol, horizonCol, zenithCol, -viewDir, v_fogColor, v_underwaterRainTime.z, rainFactor, false, underWater, false)*1.0;

  skyColor = colorCorrection(skyColor);
  
  skyColor += pow(vec3_splat(star(sPos.zx*250.0, v_underwaterRainTime.z))*1.0, vec3(16,7,5))*mask;
  
  vec3 stars_direction = normalize(v_worldPos);
    float stars_threshold = 8.0; // modifies the number of stars that are visible
    float stars_exposure = 200.0; // modifies the overall strength of the stars
    float stars = pow(clamp(noise(stars_direction * 200.0), 0.0, 1.0), stars_threshold) * stars_exposure;
    stars *= mix(0.4, 1.4, noise(stars_direction * 100.0 + vec3(ViewPositionAndTime.w, ViewPositionAndTime.w, ViewPositionAndTime.w))); // time based flickeri
    skyColor += vec3(stars*2.0, stars*2.0, stars*2.0)*mask;
  
  gl_FragColor = vec4(skyColor, 1.0);
#else
  gl_FragColor = vec4(0.0,0.0,0.0,0.0);
#endif
}

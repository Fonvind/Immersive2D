#version 330 compatibility

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;

uniform mat4 InverseTransformMatrix;
uniform mat4 ModelViewMat;
uniform vec3 CameraPos;
uniform vec3 SkyColor;
uniform vec2 LightLevel;

uniform vec3 PlaneOffset;
uniform float PlaneSlope;
uniform vec3 PlaneNormal;

in vec2 texCoord;

out vec4 fragColor;

// almost the same as in plane
float sdf(vec3 point) {
    float x = (PlaneSlope * PlaneOffset.x - PlaneOffset.z + point.x / PlaneSlope + point.z) / (PlaneSlope + 1. / PlaneSlope);
    float z = PlaneSlope * (x - PlaneOffset.x) + PlaneOffset.z;

    vec3 toPoint = vec3(point.x - x, 0, point.z - z);

    return length(toPoint) * sign(dot(toPoint, PlaneNormal));
}

void main() {
    vec4 tex = texture(DiffuseSampler, texCoord);
    vec3 finalCol = mix(vec3(0.), pow(SkyColor, vec3(2.2)), LightLevel.y/15.);

    float sceneDepth = texture(DepthSampler, texCoord).x;

    if (sceneDepth == 1.) {
        fragColor = vec4(finalCol, 1.);
        return;
    }

    vec3 ndc = vec3(texCoord.xy, sceneDepth) * 2.0 - 1.0;
    vec4 homPos = InverseTransformMatrix * vec4(ndc, 1.0);
    vec3 viewPos = homPos.xyz / homPos.w;

    vec3 worldPos = (inverse(ModelViewMat) * vec4(viewPos, 1.)).xyz + CameraPos;
    float dist = sdf(worldPos);

    // desaturate
    tex /= 1 + 0.1 * smoothstep(0.40, 0.60, dist);

    float distFactor = clamp(dist / max(LightLevel.x, LightLevel.y), 0.1, 1.);
    fragColor = vec4(mix(tex.rgb, finalCol, distFactor * smoothstep(0.3, 0.7, dist)), 1.);
}
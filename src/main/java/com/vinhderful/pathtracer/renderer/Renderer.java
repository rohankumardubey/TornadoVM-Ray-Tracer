package com.vinhderful.pathtracer.renderer;

import com.vinhderful.pathtracer.bodies.Body;
import com.vinhderful.pathtracer.scene.Camera;
import com.vinhderful.pathtracer.scene.World;
import com.vinhderful.pathtracer.utils.Hit;
import com.vinhderful.pathtracer.utils.Ray;
import com.vinhderful.pathtracer.utils.Vector3f;

/**
 * Implements functions to draw the scene on the canvas
 */
public class Renderer {

    private final int width;
    private final int height;

    private final int shadowSampleSize;
    private final int reflectionBounceLimit;

    private final int[] pixels;

    /**
     * Construct a Renderer object given the width and height of output and the World to render
     *
     * @param width                 width of output
     * @param height                height of output
     * @param world                 world to render
     * @param shadowSampleSize      number of shadow samples to take for soft shadows
     * @param reflectionBounceLimit number of recursive reflection bounces
     */
    public Renderer(int width, int height, World world, int shadowSampleSize, int reflectionBounceLimit) {
        this.width = width;
        this.height = height;
        this.pixels = new int[width * height];

        this.shadowSampleSize = shadowSampleSize;
        this.reflectionBounceLimit = reflectionBounceLimit;

        render(world);
    }

    /**
     * Construct a Renderer object given the width and height of output and the World to render
     * Shadow sample size defaults to 200, reflection bounce limits defaults to 5
     *
     * @param width  width of output
     * @param height height of output
     * @param world  world to render
     */
    public Renderer(int width, int height, World world) {
        this.width = width;
        this.height = height;
        this.pixels = new int[width * height];

        this.shadowSampleSize = 200;
        this.reflectionBounceLimit = 5;

        render(world);
    }

    /**
     * Given a ray and a world, get a hit event of the closest body the ray hits in
     * the world
     *
     * @param ray   the ray
     * @param world the world
     * @return A hit event containing the ray, the closest body and the hit
     * position, null if the ray does not hit any bodies
     */
    public static Hit getClosestHit(Ray ray, World world) {
        Hit closestHit = null;

        for (Body body : world.getBodies()) {
            if (body == null) continue;

            Vector3f intersection = body.getIntersection(ray);

            if (intersection != null && (closestHit == null
                    || closestHit.getPosition().distanceFrom(ray.getOrigin())
                    > intersection.distanceFrom(ray.getOrigin())))
                closestHit = new Hit(body, ray, intersection);
        }

        return closestHit;
    }

    /**
     * Calculate the OpenGL style x coordinate of the canvas
     * (0, 0) is the middle of the screen
     *
     * @param x the pixel's x coordinate
     * @return the normalized x coordinate
     */
    public float getNormalizedX(int x) {
        if (width > height)
            return (float) (x - width / 2 + height / 2) / height * 2 - 1;
        else
            return (float) x / width * 2 - 1;
    }

    /**
     * Calculate the OpenGL style y coordinate of the canvas
     * (0, 0) is the middle of the screen
     *
     * @param y the pixel's y coordinate
     * @return the normalized y coordinate
     */
    public float getNormalizedY(int y) {
        if (width > height)
            return -((float) y / height * 2 - 1);
        else
            return -((float) (y - height / 2 + width / 2) / width * 2 - 1);
    }

    /**
     * Render the scene given the world to render
     *
     * @param world the world to render
     */
    public void render(World world) {

        Camera camera = world.getCamera();

        // Place camera in front of viewport
        Vector3f eyePos = new Vector3f(0, 0, (float) (-1 / Math.tan(Math.toRadians(camera.getFOV() / 2))));

        // Loop over every pixel
        for (int x = 0; x < width; x++)
            for (int y = 0; y < height; y++) {

                // Shoot ray from camera to pixel
                Vector3f rayDir = new Vector3f(getNormalizedX(x), getNormalizedY(y), 0)
                        .subtract(eyePos).normalize().rotate(camera.getYaw(), camera.getPitch());
                Ray ray = new Ray(camera.getPosition(), rayDir);

                // Calculate pixel color
                Hit hit = getClosestHit(ray, world);
                if (hit != null) {
                    if (hit.getBody() == world.getLight())
                        pixels[x + y * width] = hit.getColor().toARGB();
                    else
                        pixels[x + y * width] = Shader.getPixelColor(hit, world, shadowSampleSize, reflectionBounceLimit).toARGB();
                } else
                    pixels[x + y * width] = world.getSkybox().getColor(rayDir).toARGB();
            }
    }

    /**
     * Return the ARGB pixel array
     *
     * @return the ARGB pixel array
     */
    public int[] getPixels() {
        return pixels;
    }
}
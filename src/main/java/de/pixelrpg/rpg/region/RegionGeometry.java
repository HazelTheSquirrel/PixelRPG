package de.pixelrpg.rpg.region;

import java.util.ArrayList;
import java.util.List;

/** Immutable simple polygon on the X/Z plane with validation and point containment. */
public final class RegionGeometry {
    private static final double EPSILON = 1.0E-7;
    private final List<RegionPoint> points;
    private final double area;
    private final double minX, maxX, minZ, maxZ;

    private RegionGeometry(List<RegionPoint> points) {
        this.points = List.copyOf(points);
        this.area = Math.abs(signedArea(points));
        this.minX = points.stream().mapToDouble(RegionPoint::x).min().orElseThrow();
        this.maxX = points.stream().mapToDouble(RegionPoint::x).max().orElseThrow();
        this.minZ = points.stream().mapToDouble(RegionPoint::z).min().orElseThrow();
        this.maxZ = points.stream().mapToDouble(RegionPoint::z).max().orElseThrow();
    }

    public static ValidationResult validate(List<RegionPoint> rawPoints) {
        if (rawPoints == null || rawPoints.size() < 3) return ValidationResult.invalid("Mindestens drei Punkte sind erforderlich.");
        List<RegionPoint> points = new ArrayList<>(rawPoints);
        for (RegionPoint point : points) {
            if (point == null || !Double.isFinite(point.x()) || !Double.isFinite(point.z())) {
                return ValidationResult.invalid("Die Kontur enthält ungültige Koordinaten.");
            }
        }
        for (int i = 0; i < points.size(); i++) {
            if (same(points.get(i), points.get((i + 1) % points.size()))) {
                return ValidationResult.invalid("Direkt aufeinanderfolgende Punkte dürfen nicht identisch sein.");
            }
        }
        if (Math.abs(signedArea(points)) <= EPSILON) return ValidationResult.invalid("Das Polygon hat keine gültige Fläche.");
        int n = points.size();
        for (int i = 0; i < n; i++) {
            RegionPoint a = points.get(i), b = points.get((i + 1) % n);
            for (int j = i + 1; j < n; j++) {
                if (j == i || j == (i + 1) % n || (i == 0 && j == n - 1)) continue;
                if (segmentsIntersect(a, b, points.get(j), points.get((j + 1) % n))) {
                    return ValidationResult.invalid("Die Polygonkontur überschneidet oder berührt sich selbst.");
                }
            }
        }
        return ValidationResult.valid(new RegionGeometry(points));
    }

    public List<RegionPoint> points() { return points; }
    public double area() { return area; }
    public double minX() { return minX; }
    public double maxX() { return maxX; }
    public double minZ() { return minZ; }
    public double maxZ() { return maxZ; }

    public boolean contains(double x, double z) {
        if (x < minX - EPSILON || x > maxX + EPSILON || z < minZ - EPSILON || z > maxZ + EPSILON) return false;
        boolean inside = false;
        for (int i = 0, j = points.size() - 1; i < points.size(); j = i++) {
            RegionPoint a = points.get(i), b = points.get(j);
            if (onSegment(a, b, x, z)) return true;
            if ((a.z() > z) != (b.z() > z)) {
                double intersectionX = (b.x() - a.x()) * (z - a.z()) / (b.z() - a.z()) + a.x();
                if (x < intersectionX) inside = !inside;
            }
        }
        return inside;
    }

    public record ValidationResult(boolean valid, RegionGeometry geometry, String error) {
        static ValidationResult valid(RegionGeometry geometry) { return new ValidationResult(true, geometry, null); }
        static ValidationResult invalid(String error) { return new ValidationResult(false, null, error); }
    }

    private static double signedArea(List<RegionPoint> points) {
        double sum = 0;
        for (int i = 0; i < points.size(); i++) {
            RegionPoint a = points.get(i), b = points.get((i + 1) % points.size());
            sum += a.x() * b.z() - b.x() * a.z();
        }
        return sum / 2.0;
    }
    private static boolean same(RegionPoint a, RegionPoint b) {
        return Math.abs(a.x() - b.x()) <= EPSILON && Math.abs(a.z() - b.z()) <= EPSILON;
    }
    private static double orientation(RegionPoint a, RegionPoint b, RegionPoint c) {
        return (b.x() - a.x()) * (c.z() - a.z()) - (b.z() - a.z()) * (c.x() - a.x());
    }
    private static boolean onSegment(RegionPoint a, RegionPoint b, double x, double z) {
        if (Math.abs(orientation(a, b, new RegionPoint(x, z))) > EPSILON) return false;
        return x >= Math.min(a.x(), b.x()) - EPSILON && x <= Math.max(a.x(), b.x()) + EPSILON
                && z >= Math.min(a.z(), b.z()) - EPSILON && z <= Math.max(a.z(), b.z()) + EPSILON;
    }
    private static boolean segmentsIntersect(RegionPoint a, RegionPoint b, RegionPoint c, RegionPoint d) {
        double o1 = orientation(a, b, c), o2 = orientation(a, b, d), o3 = orientation(c, d, a), o4 = orientation(c, d, b);
        if (((o1 > EPSILON && o2 < -EPSILON) || (o1 < -EPSILON && o2 > EPSILON))
                && ((o3 > EPSILON && o4 < -EPSILON) || (o3 < -EPSILON && o4 > EPSILON))) return true;
        return (Math.abs(o1) <= EPSILON && onSegment(a, b, c.x(), c.z()))
                || (Math.abs(o2) <= EPSILON && onSegment(a, b, d.x(), d.z()))
                || (Math.abs(o3) <= EPSILON && onSegment(c, d, a.x(), a.z()))
                || (Math.abs(o4) <= EPSILON && onSegment(c, d, b.x(), b.z()));
    }
}

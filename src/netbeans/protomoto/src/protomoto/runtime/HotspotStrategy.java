package protomoto.runtime;

public interface HotspotStrategy {
    Class<?> getHotspotInterface(int arity);
    Object newHotspot(int symbolCode, int arity);
}

package protomoto.runtime;

import protomoto.cell.Environment.CallSiteContainer;

public interface HotspotStrategy {
    Class<?> getHotspotInterface(int arity);
    Object newHotspot(int symbolCode, int arity);
    void bind(CallSiteContainer callSite, int callSiteId, int symbolCode, int arity);
    void bindGet(CallSiteContainer callSite, int propertyGetId, int symbolCode);
    ClassLoader getClassLoader(ClassLoader parent);
    ClassLoader getClassLoader();
}

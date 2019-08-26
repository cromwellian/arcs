package arcs.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;

public class ParticleExecutionContextImpl implements ParticleExecutionContext {
  private ParticleLoader particleLoader;
  private PortableJsonParser jsonParser;
  private ArrayList<Particle> particles;
  private HandleFactory handleFactory;

  @Inject
  public ParticleExecutionContextImpl(
      PortableJsonParser jsonParser, ParticleLoader particleLoader, HandleFactory handleFactory) {
    this.jsonParser = jsonParser;
    this.particleLoader = particleLoader;
    this.particles = new ArrayList<>();
    this.handleFactory = handleFactory;
  }

  @Override
  public Particle instantiateParticle(
      String particleId,
      ParticleSpec spec,
      Map<String, StorageProxy> proxies,
      IdGenerator idGenerator) {
    // TODO: use the full spec.implPath, instead of the filename.
    Particle particle =
        particleLoader
            .loadParticle(spec.getFileName())
            .flatMap(x -> Optional.ofNullable(x.createParticle()))
            .orElse(null);
    Logger.getGlobal().log(
        Level.SEVERE, "Arcs: Trying to load particle " + spec.getFileName() + " got " + particle);
    Objects.requireNonNull(particle).setSpec(spec);
    particle.setJsonParser(jsonParser);
    this.particles.add(particle);

    Map<String, Handle> handleMap = new HashMap<>();
    Map<Handle, StorageProxy> registerMap = new HashMap<>();

    for (String proxyName : proxies.keySet()) {
      StorageProxy storageProxy = proxies.get(proxyName);
      Handle handle =
          this.handleFactory.handleFor(
              storageProxy,
              idGenerator,
              proxyName,
              particleId,
              spec.isInput(proxyName),
              spec.isOutput(proxyName));
      handleMap.put(proxyName, handle);
      registerMap.put(handle, storageProxy);
    }

    particle.setHandles(handleMap);
    for (Handle handle : registerMap.keySet()) {
      StorageProxy storageProxy = registerMap.get(handle);
      storageProxy.register(particle, handle);
    }
    return particle;
  }
}

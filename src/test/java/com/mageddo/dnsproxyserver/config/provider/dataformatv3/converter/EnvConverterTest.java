package com.mageddo.dnsproxyserver.config.provider.dataformatv3.converter;

import com.mageddo.dnsproxyserver.config.provider.dataformatv3.ConfigV3;
import com.mageddo.dnsproxyserver.config.provider.dataformatv3.templates.ConfigV3Templates;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnvConverterTest {

  private final EnvConverter converter = new EnvConverter();

  @Test
  void shouldParseEnvironmentMatchingTemplate() {

    final Map<String, String> env = new LinkedHashMap<>();
    env.put("DPS_VERSION", "3");
    env.put("DPS_SERVER_DNS_PORT", "53");
    env.put("DPS_SERVER_DNS_NO_ENTRIES_RESPONSE_CODE", "3");
    env.put("DPS_SERVER_WEB_PORT", "5380");
    env.put("DPS_SERVER_PROTOCOL", "UDP_TCP");
    env.put("DPS_SOLVER_REMOTE_ACTIVE", "true");
    env.put("DPS_SOLVER_REMOTE_DNS_SERVERS_0", "8.8.8.8");
    env.put("DPS_SOLVER_REMOTE_DNS_SERVERS_1", "4.4.4.4:53");
    env.put("DPS_SOLVER_REMOTE_CIRCUIT_BREAKER_NAME", "STATIC_THRESHOLD");
    env.put("DPS_SOLVER_DOCKER_REGISTER_CONTAINER_NAMES", "false");
    env.put("DPS_SOLVER_DOCKER_DOMAIN", "docker");
    env.put("DPS_SOLVER_DOCKER_HOST_MACHINE_FALLBACK", "true");
    env.put("DPS_SOLVER_DOCKER_DPS_NETWORK_NAME", "dps");
    env.put("DPS_SOLVER_DOCKER_DPS_NETWORK_AUTO_CREATE", "false");
    env.put("DPS_SOLVER_DOCKER_DPS_NETWORK_AUTO_CONNECT", "false");
    env.put("DPS_SOLVER_SYSTEM_HOST_MACHINE_HOSTNAME", "host.docker");
    env.put("DPS_SOLVER_LOCAL_ACTIVE_ENV", "");
    env.put("DPS_SOLVER_LOCAL_ENVS_0_NAME", "");
    env.put("DPS_SOLVER_LOCAL_ENVS_0_HOSTNAMES_0_TYPE", "A");
    env.put("DPS_SOLVER_LOCAL_ENVS_0_HOSTNAMES_0_HOSTNAME", "github.com");
    env.put("DPS_SOLVER_LOCAL_ENVS_0_HOSTNAMES_0_IP", "192.168.0.1");
    env.put("DPS_SOLVER_LOCAL_ENVS_0_HOSTNAMES_0_TTL", "255");
    env.put("DPS_SOLVER_STUB_DOMAIN_NAME", "stub");
    env.put("DPS_DEFAULT_DNS_ACTIVE", "true");
    env.put("DPS_DEFAULT_DNS_RESOLV_CONF_PATHS", "/host/etc/systemd/resolved.conf,/host/etc/resolv.conf,/etc/systemd/resolved.conf,/etc/resolv.conf");
    env.put("DPS_DEFAULT_DNS_RESOLV_CONF_OVERRIDE_NAME_SERVERS", "true");
    env.put("DPS_LOG_LEVEL", "DEBUG");
    env.put("DPS_LOG_FILE", "console");

    final ConfigV3 expected = new ConfigV3Templates().build();
    final ConfigV3 parsed = converter.parse(env);

    assertEquals(expected, parsed);
  }

  @Test
  void shouldBuildTreeWithNestedStructure() {
    final Map<String, String> env = Map.of("DPS_SOLVER_DOCKER_DPS_NETWORK_AUTO_CONNECT", "true");

    final Map<String, Object> tree = converter.buildTree(env);

    assertEquals(
      Map.of("solver", Map.of("docker", Map.of("dpsNetwork", Map.of("autoConnect", true)))),
      tree
    );
  }
}

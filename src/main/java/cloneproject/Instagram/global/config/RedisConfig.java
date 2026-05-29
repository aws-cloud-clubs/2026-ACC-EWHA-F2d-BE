package cloneproject.Instagram.global.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@Configuration
@EnableRedisRepositories
public class RedisConfig {

	@Value("${spring.redis.host:localhost}")
	private String host;

	@Value("${spring.redis.port:6379}")
	private int port;

	@Value("${spring.redis.ssl:false}")
	private boolean ssl;

	@Value("${spring.redis.cluster.nodes:}")
	private String clusterNodes;

	@Bean
	public RedisConnectionFactory redisConnectionFactory() {
		LettuceClientConfiguration.LettuceClientConfigurationBuilder builder =
			LettuceClientConfiguration.builder();
		if (ssl) {
			builder.useSsl();
		}
		LettuceClientConfiguration clientConfig = builder.build();

		if (!clusterNodes.isEmpty()) {
			RedisClusterConfiguration clusterConfig =
				new RedisClusterConfiguration(Arrays.asList(clusterNodes.split(",")));
			return new LettuceConnectionFactory(clusterConfig, clientConfig);
		}

		RedisStandaloneConfiguration standaloneConfig = new RedisStandaloneConfiguration(host, port);
		return new LettuceConnectionFactory(standaloneConfig, clientConfig);
	}

	@Bean
	public RedisTemplate<?, ?> redisTemplate() {
		final RedisTemplate<byte[], byte[]> redisTemplate = new RedisTemplate<>();
		redisTemplate.setConnectionFactory(redisConnectionFactory());
		return redisTemplate;
	}

}

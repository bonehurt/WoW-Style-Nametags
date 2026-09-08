package com.wownpc;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Manages loading, caching, and querying NPC max hit data from the OSRS Wiki
 * Bucket API.
 */
@Slf4j
@Singleton
public class NpcMaxHitManager {
	private static final HttpUrl WIKI_BUCKET_URL = HttpUrl.parse(
			"https://oldschool.runescape.wiki/api.php?action=bucket&format=json&query=bucket('infobox_monster').select('id','max_hit').limit(5000).run()&smaxage=86400");

	private static final Pattern LEADING_DIGITS = Pattern.compile("^(\\d+)");
	private static final Pattern PAREN_DIGITS = Pattern.compile("^\\((\\d+)");
	private static final Pattern HTML_TAGS = Pattern.compile("<[^>]+>");
	private static final Pattern LEADING_CHARS = Pattern.compile("^[\\s*~]+");

	private final OkHttpClient httpClient;
	private final Gson gson;
	private final Map<Integer, Integer> maxHitCache = new ConcurrentHashMap<>();

	private volatile boolean loaded = false;
	private volatile boolean loading = false;

	@Inject
	public NpcMaxHitManager(OkHttpClient httpClient, Gson gson) {
		this.httpClient = httpClient;
		this.gson = gson;
	}

	/**
	 * Asynchronously fetches and parses NPC max hit data from the OSRS Wiki if not
	 * already loaded.
	 */
	public void preloadAll() {
		if (loaded || loading || WIKI_BUCKET_URL == null) {
			return;
		}

		loading = true;

		Request request = new Request.Builder()
				.url(WIKI_BUCKET_URL)
				.header("User-Agent", "RuneLite wow-style-nametags")
				.build();

		httpClient.newCall(request).enqueue(new Callback() {
			@Override
			public void onFailure(Call call, IOException e) {
				log.warn("Failed to load NPC max hit data from OSRS Wiki", e);
				loading = false;
			}

			@Override
			public void onResponse(Call call, Response response) {
				try (Response res = response) {
					if (!res.isSuccessful()) {
						log.warn("OSRS Wiki returned code {} when loading NPC max hit data", res.code());
						return;
					}

					ResponseBody body = res.body();
					if (body == null) {
						return;
					}

					try (Reader reader = body.charStream()) {
						parseResponse(reader);
						loaded = true;
						log.debug("Successfully loaded {} NPC max hit entries from OSRS Wiki", maxHitCache.size());
					}
				} catch (Exception e) {
					log.warn("Error parsing NPC max hit data from OSRS Wiki", e);
				} finally {
					loading = false;
				}
			}
		});
	}

	private void parseResponse(Reader reader) {
		JsonObject root = gson.fromJson(reader, JsonObject.class);
		if (root == null || !root.has("bucket")) {
			return;
		}

		JsonElement bucketElem = root.get("bucket");
		if (!bucketElem.isJsonArray()) {
			return;
		}

		JsonArray bucket = bucketElem.getAsJsonArray();
		for (JsonElement elem : bucket) {
			if (!elem.isJsonObject()) {
				continue;
			}

			JsonObject obj = elem.getAsJsonObject();
			if (!obj.has("id") || !obj.has("max_hit")) {
				continue;
			}

			JsonElement idElem = obj.get("id");
			JsonElement hitElem = obj.get("max_hit");
			if (!idElem.isJsonArray() || !hitElem.isJsonArray()) {
				continue;
			}

			JsonArray idArr = idElem.getAsJsonArray();
			JsonArray hitArr = hitElem.getAsJsonArray();

			int maxHit = -1;
			for (JsonElement h : hitArr) {
				if (h == null || !h.isJsonPrimitive()) {
					continue;
				}

				String rawHit = h.getAsString();
				if (rawHit == null || rawHit.trim().isEmpty()) {
					continue;
				}

				String[] lines = rawHit.split("\\r?\\n");
				for (String line : lines) {
					String trimmed = HTML_TAGS.matcher(line).replaceAll("").trim();
					trimmed = LEADING_CHARS.matcher(trimmed).replaceAll("");

					Matcher m = LEADING_DIGITS.matcher(trimmed);
					if (m.find()) {
						try {
							int val = Integer.parseInt(m.group(1));
							if (val > maxHit) {
								maxHit = val;
							}
						} catch (NumberFormatException ignored) {
						}
					} else {
						Matcher m2 = PAREN_DIGITS.matcher(trimmed);
						if (m2.find()) {
							try {
								int val = Integer.parseInt(m2.group(1));
								if (val > maxHit) {
									maxHit = val;
								}
							} catch (NumberFormatException ignored) {
							}
						}
					}
				}
			}

			if (maxHit >= 0) {
				for (JsonElement idVal : idArr) {
					if (idVal == null || !idVal.isJsonPrimitive()) {
						continue;
					}

					try {
						int npcId = Integer.parseInt(idVal.getAsString().trim());
						maxHitCache.put(npcId, maxHit);
					} catch (NumberFormatException ignored) {
					}
				}
			}
		}
	}

	/**
	 * Returns the max hit for the given NPC ID, or -1 if unknown.
	 */
	public int getMaxHit(int npcId) {
		Integer hit = maxHitCache.get(npcId);
		return hit != null ? hit : -1;
	}

	/**
	 * Clears the cached max hit data and resets loading state.
	 */
	public void clearCache() {
		maxHitCache.clear();
		loaded = false;
		loading = false;
	}

	/**
	 * Returns whether wiki data has been loaded into memory.
	 */
	public boolean isLoaded() {
		return loaded;
	}
}

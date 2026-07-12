import assert from 'node:assert/strict';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';
import { HttpBinClient } from '@lib/api/HttpBinClient';
import { MatchersV3, PactV3 } from '@pact-foundation/pact';
import { request } from '@playwright/test';

/**
 * Real consumer contract test (Pact JS).
 *
 * Runs OUTSIDE the Playwright projects via its own node:test runner
 * (`npm run test:contract`). It stands up Pact's mock provider, points the real
 * `HttpBinClient` (lib/api) at the mock URL, exercises live requests, and lets
 * Pact write the consumer contract to `pacts/` on success. This complements —
 * but does not replace — the Zod schema validation used elsewhere.
 */

const { like, string } = MatchersV3;

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const pactDir = path.resolve(__dirname, '..', 'pacts');

function newProvider(): PactV3 {
	return new PactV3({
		consumer: 'hlpw-client',
		provider: 'httpbin',
		dir: pactDir,
	});
}

test('httpbin consumer contract', async (t) => {
	await t.test('GET /ip returns the caller origin', async () => {
		const provider = newProvider();

		provider
			.uponReceiving('a request for the caller origin IP')
			.withRequest({ method: 'GET', path: '/ip' })
			.willRespondWith({
				status: 200,
				headers: { 'Content-Type': 'application/json' },
				body: like({ origin: string('127.0.0.1') }),
			});

		await provider.executeTest(async (mockServer) => {
			const ctx = await request.newContext({ baseURL: mockServer.url });
			const client = new HttpBinClient(ctx);

			const response = await client.getIp();
			response.expectStatus(200);

			const body = await response.json();
			assert.equal(typeof body.origin, 'string');

			await ctx.dispose();
		});
	});

	await t.test('GET /uuid returns a uuid string', async () => {
		const provider = newProvider();

		provider
			.uponReceiving('a request for a generated uuid')
			.withRequest({ method: 'GET', path: '/uuid' })
			.willRespondWith({
				status: 200,
				headers: { 'Content-Type': 'application/json' },
				body: like({
					uuid: MatchersV3.regex(
						'[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}',
						'f8a1b2c3-1234-4abc-8def-0123456789ab',
					),
				}),
			});

		await provider.executeTest(async (mockServer) => {
			const ctx = await request.newContext({ baseURL: mockServer.url });
			const client = new HttpBinClient(ctx);

			const response = await client.getUuid();
			response.expectStatus(200);

			const body = await response.json();
			assert.match(
				body.uuid,
				/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/,
			);

			await ctx.dispose();
		});
	});
});

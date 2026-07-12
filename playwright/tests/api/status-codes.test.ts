import test from '@lib/ApiBaseTest';

test.describe('Status Codes', () => {
	for (const code of [200, 201, 400, 404, 500]) {
		test(`returns status ${code}`, async ({ httpBin }) => {
			const response = await httpBin.getStatus(code);
			response.expectStatus(code);
		});
	}
});

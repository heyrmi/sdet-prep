import path from 'node:path';
import test from '@lib/BaseTest';
import { expect } from '@playwright/test';

test.describe('File Upload Page Testing', () => {
	test.beforeEach(async ({ fileUploadPage }) => {
		await fileUploadPage.goto();
	});

	test('should display correct heading', async ({ fileUploadPage }) => {
		await expect(fileUploadPage.heading).toContainText('File Uploader');
	});

	test('should upload file sucessfully', async ({ fileUploadPage }) => {
		const filePath = path.resolve('testData/files/file.txt');
		await fileUploadPage.uploadFile(filePath);

		// verify upload is success.
		await expect(fileUploadPage.successMessage).toBeVisible();
		expect(await fileUploadPage.getUploadedFileName()).toContain('file.txt');
	});

	test("should upload 'pdf' file with relative path", async ({
		fileUploadPage,
	}) => {
		const filePath = path.resolve('testData/files/sample.pdf');

		await fileUploadPage.uploadFile(filePath);
		await expect(fileUploadPage.successMessage).toBeVisible();
		expect(await fileUploadPage.getUploadedFileName()).toContain('sample.pdf');
	});
});

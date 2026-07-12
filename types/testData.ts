export interface LoginTestData {
	testName: string;
	username: string;
	password: string;
	expectedMessage: string;
	shouldSucceed: boolean;
}

export interface TableTestData {
	lastName: string;
	expectedEmail: string;
	expectedDue: string;
}

export interface HttpBinResponse {
	args: Record<string, string>;
	headers: Record<string, string>;
	origin: string;
	url: string;
}

export interface HttpBinGetResponse extends HttpBinResponse {}

export interface HttpBinPostResponse extends HttpBinResponse {
	data: string;
	files: Record<string, string>;
	form: Record<string, string>;
	json: Record<string, unknown> | null;
}

export interface HttpBinAuthResponse {
	authenticated: boolean;
	user: string;
}

export interface HttpBinCookiesResponse {
	cookies: Record<string, string>;
}

export interface HttpBinHeadersResponse {
	headers: Record<string, string>;
}

export interface HttpBinRedirectResponse {
	url: string;
}

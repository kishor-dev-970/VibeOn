import { LoginManager, AccessToken } from 'react-native-fbsdk-next';

export async function facebookLogin(): Promise<string> {
  LoginManager.setLoginBehavior('WEB_ONLY' as any);

  const result = await LoginManager.logInWithPermissions([
    'public_profile',
    'user_friends',
  ]);
  if (result.isCancelled) {
    throw new Error('Facebook login was cancelled');
  }
  const data = await AccessToken.getCurrentAccessToken();
  if (!data?.accessToken) {
    throw new Error('Could not obtain Facebook access token');
  }
  return data.accessToken;
}

export function facebookLogout(): void {
  LoginManager.logOut();
}

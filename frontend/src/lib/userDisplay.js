export function shortUserId(userId) {
  if (!userId) return "—";
  return userId.split("-")[0];
}

export function buildUserProfileMap(profiles = [], currentUser) {
  const map = {};
  if (currentUser?.userId) {
    map[currentUser.userId] = {
      userId: currentUser.userId,
      fullName: currentUser.fullName,
      email: currentUser.email,
    };
  }

  for (const profile of profiles) {
    if (profile?.userId) map[profile.userId] = profile;
  }

  return map;
}

export function userDisplayName(userId, profilesByUserId = {}) {
  if (!userId) return "—";
  const profile = profilesByUserId[userId];
  return profile?.fullName?.trim() || profile?.email || shortUserId(userId);
}

export function userOptionLabel(userId, profilesByUserId = {}, currentUserId) {
  const name = userDisplayName(userId, profilesByUserId);
  return userId && userId === currentUserId ? `${name} (you)` : name;
}

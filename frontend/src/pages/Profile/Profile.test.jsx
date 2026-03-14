import { render, screen, cleanup, act } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import Profile from "./Profile";
import { AppContext } from "../../context/AppContext";

vi.mock("../../assets/assets", () => ({
  assets: {
    noProfilePic: "mocked-no-profile-pic",
    instagramPhoto: "mocked-instagram-photo",
    more: "mocked-more",
    more_media: "mocked-more-media",
  },
}));

vi.mock("../../api/userApi", () => ({
  fetchProfileInfo: vi.fn(() =>
    Promise.resolve({
      id: 1,
      username: "mihajlotim",
      fname: "Mihajlo",
      lname: "Timotijevic",
      bio: "Ja sam Mihajlo i vozim biciklu.",
      profilePictureUrl: null,
      privateProfile: false,
    }),
  ),
  getUserAvatarUrl: vi.fn(() => "mocked-avatar-url"),
}));

vi.mock("../../api/followApi", () => ({
  fetchFollowCount: vi.fn(() =>
    Promise.resolve({ followersCount: 10, followingCount: 5 }),
  ),
  checkFollow: vi.fn(() =>
    Promise.resolve({ following: false, pending: false }),
  ),
  toggleFollow: vi.fn(),
}));

vi.mock("../../api/postApi", () => ({
  fetchPosts: vi.fn(() => Promise.resolve([])),
  fetchPostCount: vi.fn(() => Promise.resolve({ count: 0 })),
  getPostMediaUrl: vi.fn((url) => url),
}));

vi.mock("../../api/blockApi", () => ({
  checkBlock: vi.fn(() => Promise.resolve({ blocked: false })),
  toggleBlock: vi.fn(),
}));

vi.mock("../../components/SinglePost/SinglePost", () => ({
  default: () => <div>SinglePost</div>,
}));

vi.mock("../../components/FollowersModal/FollowersModal", () => ({
  default: () => <div>FollowersModal</div>,
}));

vi.mock("../../components/CustomConfirm/CustomConfirm", () => ({
  default: ({ message, onConfirm, onCancel }) => (
    <div>
      <span>{message}</span>
      <button onClick={onConfirm}>Потврди</button>
      <button onClick={onCancel}>Откажи</button>
    </div>
  ),
}));

const mockUser = { username: "mihajlotim" };
const mockSetOnPostCreated = vi.fn();

const renderProfile = async (
  username = "mihajlotim",
  contextUser = mockUser,
) => {
  await act(async () => {
    render(
      <MemoryRouter initialEntries={[`/profile/${username}`]}>
        <AppContext.Provider
          value={{ user: contextUser, setOnPostCreated: mockSetOnPostCreated }}
        >
          <Routes>
            <Route path="/profile/:username" element={<Profile />} />
          </Routes>
        </AppContext.Provider>
      </MemoryRouter>,
    );
  });
};

beforeEach(() => {
  localStorage.setItem("token", "test-token");
});

afterEach(() => {
  cleanup();
  vi.resetAllMocks();
  localStorage.clear();
});

describe("Profile", () => {
  it("prikazuje loading dok se ucitava profil", async () => {
    const { fetchProfileInfo } = await import("../../api/userApi");
    vi.mocked(fetchProfileInfo).mockImplementation(() => new Promise(() => {}));

    render(
      <MemoryRouter initialEntries={["/profile/mihajlotim"]}>
        <AppContext.Provider
          value={{ user: mockUser, setOnPostCreated: mockSetOnPostCreated }}
        >
          <Routes>
            <Route path="/profile/:username" element={<Profile />} />
          </Routes>
        </AppContext.Provider>
      </MemoryRouter>,
    );
    expect(screen.getByText("Учитава се...")).toBeInTheDocument();
  });

  it("prikazuje informacije o profilu", async () => {
    await renderProfile();
    expect(screen.getByText("mihajlotim")).toBeInTheDocument();
    expect(screen.getByText("Mihajlo Timotijevic")).toBeInTheDocument();
    expect(
      screen.getByText("Ja sam Mihajlo i vozim biciklu."),
    ).toBeInTheDocument();
  });

  it("prikazuje broj pratilaca i pracenja", async () => {
    await renderProfile();
    const numbers = screen.getAllByText("10");
    expect(numbers.length).toBeGreaterThan(0);
    const numbers2 = screen.getAllByText("5");
    expect(numbers2.length).toBeGreaterThan(0);
  });

  it("prikazuje dugme Измените профил za sopstveni profil", async () => {
    await renderProfile();
    expect(screen.getByText("Измените профил")).toBeInTheDocument();
  });

  it("prikazuje dugme Прати za tudji profil", async () => {
    await renderProfile("mihajlotim", { username: "otheruser" });
    expect(screen.getByText("Прати")).toBeInTheDocument();
  });

  it("prikazuje poruku kad nema objava", async () => {
    await renderProfile();
    expect(screen.getByText("Поделите фотографије")).toBeInTheDocument();
  });

  it("prikazuje da je nalog privatan za tudji profil", async () => {
    const { fetchProfileInfo } = await import("../../api/userApi");
    fetchProfileInfo.mockResolvedValueOnce({
      id: 1,
      username: "mihajlotim",
      fname: "Mihajlo",
      lname: "Timotijevic",
      bio: "Ja sam Mihajlo i vozim biciklu.",
      profilePictureUrl: null,
      privateProfile: true,
    });

    await renderProfile("mihajlotim", { username: "otheruser" });
    expect(screen.getByText("Овај налог је приватан")).toBeInTheDocument();
  });
});

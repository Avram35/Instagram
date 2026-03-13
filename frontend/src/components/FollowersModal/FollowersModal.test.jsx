import { render, screen, cleanup, act } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import FollowersModal from "./FollowersModal";
import { AppContext } from "../../context/AppContext";

vi.mock("../FollowerRow/FollowerRow", () => ({
  default: ({ userId }) => <div data-testid="follower-row">{userId}</div>,
}));

const mockUser = { id: 1, username: "mihajlotim" };
const followersModalRef = { current: document.createElement("div") };
const mockOnClose = vi.fn();

const mockFollowers = [
  { followerId: 2, followingId: 1 },
  { followerId: 3, followingId: 1 },
];

const renderModal = async (
  followersFollowing = "Пратиоци",
  list = mockFollowers,
) => {
  global.fetch = vi.fn((url) => {
    if (url.includes("/followers") || url.includes("/following")) {
      return Promise.resolve({
        json: () => Promise.resolve(list),
      });
    }
    if (url.includes("/check-internal/")) {
      return Promise.resolve({
        json: () => Promise.resolve({ following: false }),
      });
    }
    return Promise.resolve({ json: () => Promise.resolve({}) });
  });

  await act(async () => {
    render(
      <AppContext.Provider value={{ user: mockUser }}>
        <FollowersModal
          followersModalRef={followersModalRef}
          followersFollowing={followersFollowing}
          profileUserId={1}
          isOwnProfile={true}
          onClose={mockOnClose}
        />
      </AppContext.Provider>,
    );
  });
};

beforeEach(() => {
  localStorage.setItem("token", "test-token");
  vi.clearAllMocks();
});

afterEach(() => {
  cleanup();
  localStorage.clear();
});

describe("FollowersModal", () => {
  it("prikazuje naslov Пратиоци", async () => {
    await renderModal("Пратиоци");
    expect(screen.getByText("Пратиоци")).toBeInTheDocument();
  });

  it("prikazuje naslov Пратите", async () => {
    await renderModal("Пратите", [{ followerId: 1, followingId: 2 }]);
    expect(screen.getByText("Пратите")).toBeInTheDocument();
  });

  it("prikazuje poruku kad nema korisnika", async () => {
    await renderModal("Пратиоци", []);
    expect(screen.getByText("Нема корисника")).toBeInTheDocument();
  });

  it("prikazuje listu pratilaca", async () => {
    await renderModal("Пратиоци");
    expect(screen.getAllByTestId("follower-row").length).toBe(2);
  });
});

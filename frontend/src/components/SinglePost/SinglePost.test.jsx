import {
  render,
  screen,
  cleanup,
  act,
  fireEvent,
} from "@testing-library/react";
import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import SinglePost from "./SinglePost";
import { AppContext } from "../../context/AppContext";

vi.mock("../../api/userApi", () => ({
  fetchUserById: vi.fn(() =>
    Promise.resolve({
      id: 1,
      username: "mihajlotim",
      profilePictureUrl: null,
    }),
  ),
  getUserAvatarUrl: vi.fn(() => "mocked-avatar-url"),
}));

vi.mock("../../api/likeApi", () => ({
  fetchLikeCount: vi.fn(() => Promise.resolve({ count: 5 })),
  checkLiked: vi.fn(() => Promise.resolve({ liked: false })),
  toggleLike: vi.fn(),
}));

vi.mock("../../api/commentApi", () => ({
  fetchCommentCount: vi.fn(() => Promise.resolve({ count: 3 })),
  fetchCommentList: vi.fn(() => Promise.resolve([])),
  addComment: vi.fn(),
  deleteComment: vi.fn(),
  updateComment: vi.fn(),
}));

vi.mock("../../api/postApi", () => ({
  updatePost: vi.fn(),
  deletePost: vi.fn(),
  deletePostMedia: vi.fn(),
  getPostMediaUrl: vi.fn((url) => url),
}));

vi.mock("../../assets/assets", () => ({
  assets: {
    noProfilePic: "mocked-no-profile-pic",
    more: "mocked-more",
    heart: "mocked-heart",
    heart_filled: "mocked-heart-filled",
    comment: "mocked-comment",
  },
}));

vi.mock("./PostMedia", () => ({
  default: ({ currentPost }) => (
    <div data-testid="post-media">
      <img src={currentPost.media[0].mediaUrl} alt="media" />
    </div>
  ),
}));

vi.mock("./PostHeader", () => ({
  default: ({ author }) => (
    <div data-testid="post-header">
      <span>{author.username}</span>
    </div>
  ),
}));

vi.mock("./PostDescription", () => ({
  default: ({ currentPost, author }) => (
    <div data-testid="post-description">
      <span>{author.username}</span>
      <span>{currentPost.description}</span>
      <span>{currentPost.createdAt?.slice(0, 10)}</span>
    </div>
  ),
}));

vi.mock("./PostLikes", () => ({
  default: ({ likeCounts, commentCounts }) => (
    <div data-testid="post-likes">
      <span>{likeCounts} Свиђања</span>
      <span>{commentCounts} коментара</span>
    </div>
  ),
}));

vi.mock("./PostCommentInput", () => ({
  default: ({ commentInput, setCommentInput, handleAddComment }) => (
    <div data-testid="post-comment-input">
      <input
        placeholder="Унесите коментар..."
        value={commentInput}
        onChange={(e) => setCommentInput(e.target.value)}
      />
      <button onClick={handleAddComment}>Објави</button>
    </div>
  ),
}));

vi.mock("./CommentItem", () => ({
  default: ({ comment }) => (
    <div data-testid="comment-item">
      <span>{comment.text}</span>
    </div>
  ),
}));

vi.mock("../CustomConfirm/CustomConfirm", () => ({
  default: ({ message, onConfirm, onCancel }) => (
    <div>
      <span>{message}</span>
      <button onClick={onConfirm}>Потврди</button>
      <button onClick={onCancel}>Откажи</button>
    </div>
  ),
}));

const mockPostInfo = {
  id: 1,
  userId: 1,
  description: "Muke moje niko ne zna!",
  createdAt: "2024-01-15T10:00:00",
  media: [{ mediaUrl: "/media/test.jpg" }],
};

const mockUser = {
  id: 1,
  username: "mihajlotim",
};

const singlePostRef = { current: document.createElement("div") };

const renderSinglePost = async (postInfo = mockPostInfo) => {
  await act(async () => {
    render(
      <AppContext.Provider value={{ user: mockUser }}>
        <SinglePost singlePostRef={singlePostRef} postInfo={postInfo} />
      </AppContext.Provider>,
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

describe("SinglePost", () => {
  it("prikazuje loading dok se ucitava autor", async () => {
    const { fetchUserById } = await import("../../api/userApi");
    vi.mocked(fetchUserById).mockImplementation(() => new Promise(() => {}));

    render(
      <AppContext.Provider value={{ user: mockUser }}>
        <SinglePost singlePostRef={singlePostRef} postInfo={mockPostInfo} />
      </AppContext.Provider>,
    );
    expect(screen.getByText("Loading...")).toBeInTheDocument();
  });

  it("prikazuje username autora", async () => {
    await renderSinglePost();
    expect(screen.getAllByText("mihajlotim").length).toBeGreaterThan(0);
  });

  it("prikazuje opis objave", async () => {
    await renderSinglePost();
    expect(screen.getByText("Muke moje niko ne zna!")).toBeInTheDocument();
  });

  it("prikazuje datum objave", async () => {
    await renderSinglePost();
    expect(screen.getByText("2024-01-15")).toBeInTheDocument();
  });

  it("prikazuje broj svidjanja", async () => {
    await renderSinglePost();
    expect(screen.getByText("5 Свиђања")).toBeInTheDocument();
  });

  it("prikazuje input za komentar", async () => {
    await renderSinglePost();
    expect(
      screen.getByPlaceholderText("Унесите коментар..."),
    ).toBeInTheDocument();
  });

  it("ne prikazuje karusel dugmice za jednu sliku", async () => {
    await renderSinglePost();
    expect(document.querySelector(".carousel_btn_left")).toBeNull();
    expect(document.querySelector(".carousel_btn_right")).toBeNull();
  });
});

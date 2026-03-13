import { render, screen, cleanup, act } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import SinglePost from "./SinglePost";
import { AppContext } from "../../context/AppContext";

const mockAuthor = {
  id: 1,
  username: "mihajlotim",
  profilePictureUrl: null,
};

const mockPostInfo = {
  id: 1,
  userId: 1,
  description: "Muke moje niko ne zna!",
  createdAt: "2024-01-15T10:00:00",
  media: [{ mediaUrl: "/media/test.jpg" }],
};

const singlePostRef = { current: document.createElement("div") };

const renderSinglePost = async (postInfo = mockPostInfo) => {
  await act(async () => {
    render(<SinglePost singlePostRef={singlePostRef} postInfo={postInfo} />);
  });
};

beforeEach(() => {
  global.fetch = vi.fn((url) => {
    if (url.includes("/api/v1/user/id/")) {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve(mockAuthor),
      });
    }
    if (url.includes("/api/v1/like/count/")) {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ count: 5 }),
      });
    }
    if (url.includes("/api/v1/comment/count/")) {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ count: 3 }),
      });
    }
  });
  localStorage.setItem("token", "test-token");
});

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
  localStorage.clear();
});

describe("SinglePost", () => {
  it("prikazuje loading dok se ucitava autor", () => {
    global.fetch = vi.fn(() => new Promise(() => {}));
    render(
      <SinglePost singlePostRef={singlePostRef} postInfo={mockPostInfo} />,
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

  it("ne prikazuje karusel dugmece za jednu sliku ili video", async () => {
    await renderSinglePost();
    expect(document.querySelector(".carousel_btn_left")).toBeNull();
    expect(document.querySelector(".carousel_btn_right")).toBeNull();
  });
});

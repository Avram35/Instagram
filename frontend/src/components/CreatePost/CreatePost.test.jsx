import {
  render,
  screen,
  fireEvent,
  cleanup,
  act,
} from "@testing-library/react";
import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import CreatePost from "./CreatePost";

const mockSetCreatePost = vi.fn();
const createPostRef = { current: document.createElement("div") };

const renderCreatePost = async () => {
  await act(async () => {
    render(
      <CreatePost
        createPostRef={createPostRef}
        setCreatePost={mockSetCreatePost}
      />,
    );
  });
};

beforeEach(() => {
  global.fetch = vi.fn(() =>
    Promise.resolve({
      ok: true,
      json: () => Promise.resolve({}),
    }),
  );
  localStorage.setItem("token", "test-token");
  vi.clearAllMocks();
});

afterEach(() => {
  cleanup();
  localStorage.clear();
});

describe("CreatePost", () => {
  it("prikazuje korak 1 po defaultu", async () => {
    await renderCreatePost();
    expect(screen.getByText("Направите нову објаву")).toBeInTheDocument();
    expect(screen.getByText("Изаберите са рачунара")).toBeInTheDocument();
  });

  it("ne prikazuje dugme Подели u koraku 1", async () => {
    await renderCreatePost();
    expect(screen.queryByText("Подели")).not.toBeInTheDocument();
  });

  it("prelazi na korak 2 nakon odabira fajla", async () => {
    await renderCreatePost();
    const file = new File(["test"], "test.jpg", { type: "image/jpeg" });
    const input = document.getElementById("postFileInput");
    await act(async () => {
      fireEvent.change(input, { target: { files: [file] } });
    });
    expect(screen.getByText("Подели")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Додајте опис...")).toBeInTheDocument();
  });

  it("prikazuje back dugme u koraku 2", async () => {
    await renderCreatePost();
    const file = new File(["test"], "test.jpg", { type: "image/jpeg" });
    const input = document.getElementById("postFileInput");
    await act(async () => {
      fireEvent.change(input, { target: { files: [file] } });
    });
    expect(document.querySelector(".back_btn")).toBeInTheDocument();
  });

  it("vraca na korak 1 klikom na back dugme", async () => {
    await renderCreatePost();
    const file = new File(["test"], "test.jpg", { type: "image/jpeg" });
    const input = document.getElementById("postFileInput");
    await act(async () => {
      fireEvent.change(input, { target: { files: [file] } });
    });
    const backBtn = document.querySelector(".back_btn");
    await act(async () => {
      fireEvent.click(backBtn);
    });
    expect(screen.getByText("Изаберите са рачунара")).toBeInTheDocument();
  });

  it("menja opis", async () => {
    await renderCreatePost();
    const file = new File(["test"], "test.jpg", { type: "image/jpeg" });
    const input = document.getElementById("postFileInput");
    await act(async () => {
      fireEvent.change(input, { target: { files: [file] } });
    });
    const textarea = screen.getByPlaceholderText("Додајте опис...");
    fireEvent.change(textarea, { target: { value: "Test opis" } });
    expect(textarea).toHaveValue("Test opis");
  });

  it("poziva fetch pri submit", async () => {
    await renderCreatePost();
    const file = new File(["test"], "test.jpg", { type: "image/jpeg" });
    const input = document.getElementById("postFileInput");
    await act(async () => {
      fireEvent.change(input, { target: { files: [file] } });
    });
    await act(async () => {
      fireEvent.click(screen.getByText("Подели"));
    });
    expect(global.fetch).toHaveBeenCalled();
  });
});
